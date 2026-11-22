package com.jobtracker;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TrackerIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-16T00:30:00Z");

    @Container
    static final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:17.12-bookworm");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry settings) {
        settings.add("spring.datasource.url", database::getJdbcUrl);
        settings.add("spring.datasource.username", database::getUsername);
        settings.add("spring.datasource.password", database::getPassword);
    }

    @MockitoBean Clock clock;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate sql;

    @BeforeEach
    void reset() {
        when(clock.instant()).thenReturn(NOW);
        sql.execute("truncate roles cascade");
        sql.update("update app_settings set time_zone = 'UTC', theme = 'LIGHT'");
    }

    @Test
    void simplifyingTrackerPreservesExistingDataAndTimeZone() {
        String schema = "migration_upgrade";
        Flyway.configure().dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                .schemas(schema).defaultSchema(schema).target("2").load().migrate();
        sql.update("update migration_upgrade.workspaces set time_zone = 'Europe/Warsaw'");
        UUID roleId = UUID.randomUUID();
        UUID interviewId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        sql.update("""
            insert into migration_upgrade.roles
                (id, workspace_id, company, title, stage, substage, version, created_at, updated_at, last_activity_at,
                 stage_changed_at, next_action_text, next_action_due_on)
            values (?, 'a1d86a7c-6c20-462e-8d2e-4fcee925a10a', 'Acme', 'Engineer', 'IN_PROGRESS', 'FINAL_INTERVIEW', 4,
                    current_timestamp, current_timestamp, current_timestamp, current_timestamp,
                    'Email recruiter', date '2026-11-25')
            """, roleId);
        sql.update("""
            insert into migration_upgrade.interviews
                (id, role_id, title, type, status, version, created_at, updated_at)
            values (?, ?, 'First round', 'TECHNICAL', 'PLANNED', 0, current_timestamp, current_timestamp)
            """, interviewId, roleId);
        sql.update("""
            insert into migration_upgrade.assignments
                (id, role_id, title, status, due_on, version, created_at, updated_at,
                 brief, brief_url, work_url, submission_url, work_notes)
            values (?, ?, 'Exercise', 'PENDING', date '2026-11-25', 0, current_timestamp, current_timestamp,
                    'Build an API', 'https://example.com/task', 'https://example.com/repo',
                    'https://example.com/submission', 'Existing notes')
            """, assignmentId, roleId);
        UUID submissionOnlyId = UUID.randomUUID();
        sql.update("""
            insert into migration_upgrade.assignments
                (id, role_id, title, status, due_on, version, created_at, updated_at, submission_url)
            values (?, ?, 'Submission only', 'PENDING', null, 0, current_timestamp,
                    current_timestamp, 'https://example.com/solution')
            """, submissionOnlyId, roleId);
        sql.update("""
            insert into migration_upgrade.role_events (id, role_id, kind, occurred_at, created_at, body, details)
            values (?, ?, 'NOTE', current_timestamp, current_timestamp, 'Existing note',
                '{"before":{"stage":"APPLIED","substage":null,"outcome":null},
                  "after":{"stage":"IN_PROGRESS","substage":"FINAL_INTERVIEW","outcome":null}}'::jsonb)
            """, UUID.randomUUID(), roleId);
        UUID completedId = UUID.randomUUID();
        sql.update("""
            insert into migration_upgrade.roles
                (id, workspace_id, company, title, stage, previous_stage, previous_substage,
                 outcome, completed_at, version, created_at, updated_at, last_activity_at, stage_changed_at)
            values (?, 'a1d86a7c-6c20-462e-8d2e-4fcee925a10a', 'Acme', 'Closed application', 'COMPLETED',
                    'IN_PROGRESS', 'FINAL_INTERVIEW', 'REJECTED', current_timestamp, 2,
                    current_timestamp, current_timestamp, current_timestamp, current_timestamp)
            """, completedId);

        Flyway.configure().dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                .schemas(schema).defaultSchema(schema).load().migrate();

        assertThat(sql.queryForObject("select time_zone from migration_upgrade.app_settings where id = 1", String.class))
                .isEqualTo("Europe/Warsaw");
        assertThat(sql.queryForObject("select theme from migration_upgrade.app_settings where id = 1", String.class))
                .isEqualTo("LIGHT");
        var role = sql.queryForMap("select * from migration_upgrade.roles where id = ?", roleId);
        assertThat(role).doesNotContainKey("workspace_id");
        assertThat(role).doesNotContainKeys("substage", "previous_stage", "previous_substage");
        assertThat(role).containsEntry("company", "Acme").containsEntry("version", 4L)
                .containsEntry("next_action_text", "Email recruiter")
                .containsEntry("status", "IN_PROGRESS").containsEntry("stage", "FINAL_INTERVIEW");
        assertThat(sql.queryForMap("select status, stage, outcome from migration_upgrade.roles where id = ?", completedId))
                .containsEntry("status", "COMPLETED").containsEntry("stage", null).containsEntry("outcome", "REJECTED");
        assertThatThrownBy(() -> sql.update("update migration_upgrade.roles set outcome = null where id = ?", completedId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(sql.queryForObject("select role_id from migration_upgrade.interviews where id = ?", UUID.class, interviewId))
                .isEqualTo(roleId);
        assertThat(sql.queryForObject("select role_id from migration_upgrade.assignments where id = ?", UUID.class, assignmentId))
                .isEqualTo(roleId);
        assertThat(sql.queryForObject("select body from migration_upgrade.role_events where role_id = ? and body = 'Existing note'", String.class, roleId))
                .isEqualTo("Existing note");
        var assignment = sql.queryForMap("select * from migration_upgrade.assignments where id = ?", assignmentId);
        assertThat(assignment).doesNotContainKeys("brief", "brief_url", "submission_url", "work_notes");
        assertThat(assignment).containsEntry("instructions", "Build an API")
                .containsEntry("instructions_url", "https://example.com/task")
                .containsEntry("work_url", "https://example.com/repo").containsEntry("notes", "Existing notes");
        assertThat(sql.queryForObject("select work_url from migration_upgrade.assignments where id = ?", String.class, submissionOnlyId))
                .isEqualTo("https://example.com/solution");
        assertThat(sql.queryForObject("select due_on from migration_upgrade.assignments where id = ?", java.sql.Date.class, submissionOnlyId))
                .isNull();
        assertThatThrownBy(() -> sql.update("update migration_upgrade.assignments set title = 'Needs a date' where id = ?", submissionOnlyId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(sql.queryForObject("select body from migration_upgrade.role_events where assignment_id = ?", String.class, assignmentId))
                .isEqualTo("Previous submission link: https://example.com/submission");
        assertThat(sql.queryForMap("""
            select details->'before'->>'status' as before_status, details->'before'->>'stage' as before_stage,
                   details->'after'->>'status' as after_status, details->'after'->>'stage' as after_stage
            from migration_upgrade.role_events where role_id = ? and details->'after' is not null
            """, roleId)).containsEntry("before_status", "APPLIED").containsEntry("before_stage", null)
                .containsEntry("after_status", "IN_PROGRESS").containsEntry("after_stage", "FINAL_INTERVIEW");
        assertThat(sql.queryForObject("""
            select count(*) from information_schema.tables
            where table_schema = 'migration_upgrade' and table_name = 'workspaces'
            """, Integer.class)).isZero();
    }

    @Test
    void themePreferencePersistsAndTimeZoneOnlyUpdatesKeepIt() throws Exception {
        mvc.perform(get("/api/settings")).andExpect(jsonPath("$.theme").value("LIGHT"));
        perform(put("/api/settings"), """
            {"timeZone":"Europe/Warsaw","theme":"DARK"}
            """, 200);
        mvc.perform(get("/api/settings")).andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.timeZone").value("Europe/Warsaw"));
        perform(put("/api/settings"), "{\"timeZone\":\"UTC\"}", 200);
        mvc.perform(get("/api/settings")).andExpect(jsonPath("$.theme").value("DARK"));
        perform(put("/api/settings"), "{\"timeZone\":\"UTC\",\"theme\":\"BLUE\"}", 400);
        mvc.perform(get("/api/export")).andExpect(jsonPath("$.settings.theme").value("DARK"));
        assertThat(sql.queryForObject("select theme from app_settings where id = 1", String.class)).isEqualTo("DARK");
    }

    @Test
    void relationshipsKeepChildrenAndTimelineEventsUnderTheirRole() throws Exception {
        String role = createRole("Acme", "Engineer");
        String other = createRole("Other", "Engineer");
        UUID roleId = UUID.fromString(role.substring(role.lastIndexOf('/') + 1));
        UUID otherId = UUID.fromString(other.substring(other.lastIndexOf('/') + 1));
        var interview = perform(post(role + "/interviews"), """
            {"title":"Technical round","type":"TECHNICAL","participants":[{"name":"Alex"}]}
            """, 201);
        var assignment = perform(post(role + "/assignments"), """
            {"title":"Exercise","dueOn":"2026-11-25","instructions":"Build an API",
             "instructionsUrl":"https://example.com/task","workUrl":"https://example.com/solution","notes":"Use Java"}
            """, 201);
        mvc.perform(get(role + "/interviews/" + interview.path("id").asText()))
                .andExpect(jsonPath("$.roleId").value(roleId.toString()))
                .andExpect(jsonPath("$.participants[0].name").value("Alex"));
        mvc.perform(get(role + "/interviews/" + interview.path("id").asText() + "/participants"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Alex"));
        mvc.perform(get(other + "/interviews/" + interview.path("id").asText() + "/participants"))
                .andExpect(status().isNotFound());
        mvc.perform(get(role + "/assignments/" + assignment.path("id").asText()))
                .andExpect(jsonPath("$.roleId").value(roleId.toString()))
                .andExpect(jsonPath("$.instructions").value("Build an API"))
                .andExpect(jsonPath("$.notes").value("Use Java"));
        mvc.perform(get(other + "/assignments/" + assignment.path("id").asText())).andExpect(status().isNotFound());
        var timeline = mvc.perform(get(role + "/timeline")).andExpect(status().isOk()).andReturn();
        for (JsonNode event : json.readTree(timeline.getResponse().getContentAsString()).path("items")) {
            assertThat(event.path("roleId").asText()).isEqualTo(roleId.toString());
            mvc.perform(get(role + "/timeline/" + event.path("id").asText()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(event.path("id").asText()));
            if (event.path("kind").asText().equals("INTERVIEW_CREATED")) {
                assertThat(event.path("interviewId").asText()).isEqualTo(interview.path("id").asText());
            }
            if (event.path("kind").asText().equals("ASSIGNMENT_CREATED")) {
                assertThat(event.path("assignmentId").asText()).isEqualTo(assignment.path("id").asText());
            }
        }
        assertThatThrownBy(() -> sql.update("""
            insert into role_events (id, role_id, interview_id, kind, occurred_at, created_at)
            values (?, ?, ?, 'INTERVIEW_CREATED', current_timestamp, current_timestamp)
            """, UUID.randomUUID(), otherId, UUID.fromString(interview.path("id").asText())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void completionSuggestionsSnoozeDismissAndResetOnProgress() throws Exception {
        String role = createRole("Orbit", "Analyst");
        createRole("Orbit", "Saved role");
        var applied = perform(patch(role), """
            {"expectedVersion":0,"status":"APPLIED"}
            """, 200);
        Instant twoWeeksLater = NOW.plus(Duration.ofDays(14));
        when(clock.instant()).thenReturn(twoWeeksLater.minusSeconds(1));
        var edited = perform(patch(role), """
            {"expectedVersion":%d,"notes":"Updated my notes"}
            """.formatted(version(applied)), 200);
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.completionSuggestions").isEmpty());

        when(clock.instant()).thenReturn(twoWeeksLater);
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.completionSuggestions.length()").value(1))
                .andExpect(jsonPath("$.completionSuggestions[0].roleTitle").value("Analyst"));
        var snoozed = perform(post(role + "/completion-suggestion"), """
            {"expectedVersion":%d,"action":"SNOOZE"}
            """.formatted(version(edited)), 200);
        Instant reminder = twoWeeksLater.plus(Duration.ofDays(14));
        assertThat(snoozed.path("completionSuggestionSnoozedUntil").asText()).isEqualTo(reminder.toString());
        assertThat(snoozed.path("status").asText()).isEqualTo("APPLIED");
        when(clock.instant()).thenReturn(reminder.minusSeconds(1));
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.completionSuggestions").isEmpty());

        when(clock.instant()).thenReturn(reminder);
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.completionSuggestions.length()").value(1));
        var dismissed = perform(post(role + "/completion-suggestion"), """
            {"expectedVersion":%d,"action":"DISMISS"}
            """.formatted(version(snoozed)), 200);
        assertThat(dismissed.path("completionSuggestionDismissed").asBoolean()).isTrue();
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.completionSuggestions").isEmpty());

        var progressed = perform(patch(role), """
            {"expectedVersion":%d,"status":"IN_PROGRESS","stage":"INTERVIEW_SCHEDULED"}
            """.formatted(version(dismissed)), 200);
        assertThat(progressed.path("completionSuggestionDismissed").asBoolean()).isFalse();
        assertThat(progressed.path("stageChangedAt").asText()).isEqualTo(reminder.toString());
        when(clock.instant()).thenReturn(reminder.plus(Duration.ofDays(14)));
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.completionSuggestions.length()").value(1));
        perform(post(role + "/complete"), """
            {"expectedVersion":%d,"outcome":"NO_RESPONSE"}
            """.formatted(version(progressed)), 200);
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.completionSuggestions").isEmpty());
    }

    @Test
    void roleTransitionsFollowUpHistoryAndSearch() throws Exception {
        String role = createRole("ACME%_!", "Engineer");
        createRole("ACME%_!", "Designer");
        createRole("ACME-ABC", "Analyst");
        mvc.perform(get("/api/roles").param("q", "cme%_!").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items.length()").value(1));
        var progressed = perform(patch(role), """
            {"expectedVersion":0,"status":"IN_PROGRESS","stage":"FIRST_INTERVIEW_PASSED"}
            """, 200);
        mvc.perform(get("/api/roles").param("status", "IN_PROGRESS").param("stage", "FIRST_INTERVIEW_PASSED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.items[0].stage").value("FIRST_INTERVIEW_PASSED"));
        long stageVersion = version(progressed);
        var exactNoOp = perform(patch(role), """
            {"expectedVersion":%d,"status":"IN_PROGRESS"}
            """.formatted(stageVersion), 200);
        assertThat(exactNoOp).isEqualTo(progressed);
        perform(patch(role), """
            {"expectedVersion":%d,"status":"APPLIED","stage":"FIRST_INTERVIEW_PASSED"}
            """.formatted(stageVersion), 400);
        var moved = perform(patch(role), """
            {"expectedVersion":%d,"status":"APPLIED"}
            """.formatted(stageVersion), 200);
        assertThat(moved.path("stage").isNull()).isTrue();
        var action = perform(put(role + "/follow-up"), """
            {"expectedVersion":%d,"followUp":{"text":"Email recruiter","dueOn":"2026-11-16"}}
            """.formatted(version(moved)), 200);
        var cleared = perform(post(role + "/follow-up/complete"), versionBody(action), 200);
        assertThat(cleared.path("followUp").isNull()).isTrue();
        perform(post(role + "/follow-up/complete"), versionBody(cleared), 409);
        mvc.perform(get(role + "/timeline"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.items[?(@.kind == 'FOLLOW_UP_COMPLETED')].details.text").value("Email recruiter"))
                .andExpect(jsonPath("$.items[?(@.kind == 'FOLLOW_UP_COMPLETED')].details.dueOn").value("2026-11-16"));
        perform(post(role + "/timeline"), """
            {"kind":"RECRUITER_CONVERSATION","body":"Discussed remote work","occurredAt":"2026-10-01T12:00:00Z"}
            """, 201);
        mvc.perform(get(role + "/timeline")).andExpect(jsonPath("$.items[4].kind").value("RECRUITER_CONVERSATION"));
    }

    @Test
    void participantOnlyEditsVersionAndRejectWrongNestedIds() throws Exception {
        String role = createRole("North", "Developer");
        String other = createRole("South", "Developer");
        var created = perform(post(role + "/interviews"), """
            {"title":"Technical screen","type":"TECHNICAL","status":"SCHEDULED","startsAt":"2026-11-17T08:30:00Z",
             "participants":[{"name":"Taylor"},{"name":"Alex"}]}
            """, 201);
        String id = created.path("id").asText();
        String path = role + "/interviews/" + id;
        String first = created.path("participants").get(0).path("id").asText();
        String second = created.path("participants").get(1).path("id").asText();
        mvc.perform(get(other + "/interviews/" + id)).andExpect(status().isNotFound());
        perform(put(other + "/interviews/" + id), """
            {"expectedVersion":0,"title":"Technical screen","type":"TECHNICAL","status":"PLANNED"}
            """, 404);

        var changed = perform(put(path), """
            {"expectedVersion":0,"title":"Technical screen","type":"TECHNICAL","status":"SCHEDULED","startsAt":"2026-11-17T08:30:00Z",
             "participants":[{"name":"Alex","id":"%s","jobTitle":"Lead"},{"name":"Taylor","id":"%s"}]}
            """.formatted(second, first), 200);
        assertThat(version(changed)).isEqualTo(1);
        assertThat(changed.path("participants").get(0).path("id").asText()).isEqualTo(second);
        perform(put(path), """
            {"expectedVersion":0,"title":"Older draft","type":"TECHNICAL","status":"PLANNED"}
            """, 409);
        perform(put(path), """
            {"expectedVersion":1,"title":"Changes must roll back","type":"TECHNICAL","status":"PASSED",
             "participants":[{"name":"One","id":"%s"},{"name":"Two","id":"%s"}]}
            """.formatted(first, first), 400);
        mvc.perform(get(path)).andExpect(jsonPath("$.title").value("Technical screen"))
                .andExpect(jsonPath("$.participants[0].jobTitle").value("Lead"))
                .andExpect(jsonPath("$.status").value("SCHEDULED")).andExpect(jsonPath("$.version").value(1));
        assertThat(perform(put(path), """
            {"expectedVersion":1,"title":"Technical screen","type":"TECHNICAL","status":"SCHEDULED","startsAt":"2026-11-17T08:30:00Z",
             "participants":[{"name":"Alex","id":"%s","jobTitle":"Lead"},{"name":"Taylor","id":"%s"}]}
            """.formatted(second, first), 200)).isEqualTo(changed);
    }

    @Test
    void localDateControlsTodayAndEarlierInterviewsBecomeOutcomes() throws Exception {
        String role = createRole("Orbit", "Analyst");
        perform(put(role + "/follow-up"), """
            {"expectedVersion":0,"followUp":{"text":"Send a note"}}
            """, 200);
        perform(post(role + "/assignments"), """
            {"title":"Timed exercise","dueOn":"2026-11-16"}
            """, 201);
        perform(post(role + "/interviews"), """
            {"title":"Hiring manager","type":"HIRING_MANAGER","status":"SCHEDULED","startsAt":"2026-11-15T22:30:00Z"}
            """, 201);
        perform(post(role + "/interviews"), """
            {"title":"Plan a panel","type":"PANEL"}
            """, 201);
        mvc.perform(get("/api/today")).andExpect(status().isOk())
                .andExpect(jsonPath("$.localDate").value("2026-11-16"))
                .andExpect(jsonPath("$.overdue[0].kind").value("RECORD_INTERVIEW_OUTCOME"))
                .andExpect(jsonPath("$.dueToday[0].kind").value("ASSIGNMENT"))
                .andExpect(jsonPath("$.unscheduled.length()").value(2));
        perform(put("/api/settings"), "{\"timeZone\":\"America/Los_Angeles\"}", 200);
        mvc.perform(get("/api/today")).andExpect(status().isOk())
                .andExpect(jsonPath("$.localDate").value("2026-11-15"))
                .andExpect(jsonPath("$.dueToday[0].kind").value("SCHEDULED_INTERVIEW"))
                .andExpect(jsonPath("$.upcoming[0].dueOn").value("2026-11-16"));
    }

    @Test
    void assignmentSubmissionAndRoleCompletionStayIndependent() throws Exception {
        String role = createRole("Orbit", "Analyst");
        var active = perform(patch(role), """
            {"expectedVersion":0,"status":"IN_PROGRESS","stage":"ASSIGNMENT_PENDING"}
            """, 200);
        var assignment = perform(post(role + "/assignments"), """
            {"title":"Exercise","dueOn":"2026-11-16","instructions":"Keep original instructions","workUrl":"https://example.com/repo"}
            """, 201);
        var interview = perform(post(role + "/interviews"), """
            {"title":"Final interview","type":"FINAL","status":"SCHEDULED","startsAt":"2026-11-17T12:00:00Z"}
            """, 201);
        var submitted = perform(put(role + "/assignments/" + assignment.path("id").asText()), """
            {"expectedVersion":0,"title":"Exercise","status":"SUBMITTED","dueOn":"2026-11-16",
             "instructions":"Keep original instructions","workUrl":"https://example.com/repo","feedback":"Clear solution"}
            """, 200);
        assertThat(submitted.path("submittedAt").asText()).isEqualTo(NOW.toString());
        assertThat(interview.path("status").asText()).isEqualTo("SCHEDULED");
        var completed = perform(post(role + "/complete"), """
            {"expectedVersion":%d,"outcome":"ACCEPTED"}
            """.formatted(version(active)), 200);
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.upcoming.length()").value(0));
        perform(post(role + "/assignments"), """
            {"title":"New on completed role","dueOn":"2026-11-17"}
            """, 409);
        var reopened = perform(post(role + "/reopen"), versionBody(completed), 200);
        assertThat(reopened.path("status").asText()).isEqualTo("SAVED");
        assertThat(reopened.path("stage").isNull()).isTrue();
        assertThat(reopened.has("previousStage")).isFalse();
        assertThat(reopened.has("previousSubstage")).isFalse();
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.upcoming[0].kind").value("SCHEDULED_INTERVIEW"))
                .andExpect(jsonPath("$.dueToday.length()").value(0));
        mvc.perform(get("/api/export")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=job-search-tracker.json"))
                .andExpect(jsonPath("$.schemaVersion").value(3)).andExpect(jsonPath("$.roles[0].title").value("Analyst"))
                .andExpect(jsonPath("$.roles[0].assignments[0].feedback").value("Clear solution"))
                .andExpect(jsonPath("$.roles[0].timeline.length()").value(7));
    }

    @Test
    void nextScheduledSortHappensBeforePaginationAndWritesAreLocalJson() throws Exception {
        String noSchedule = createRole("Orbit", "No schedule");
        String later = createRole("Orbit", "Later");
        String earlier = createRole("Orbit", "Earlier");
        perform(post(later + "/interviews"), """
            {"title":"Scheduled","type":"FINAL","status":"SCHEDULED","startsAt":"2026-11-19T12:00:00Z"}
            """, 201);
        perform(put(earlier + "/follow-up"), """
            {"expectedVersion":0,"followUp":{"text":"Send a note","dueOn":"2026-11-18"}}
            """, 200);
        for (var expected : List.of(Map.entry("Earlier", "0"), Map.entry("Later", "1"), Map.entry("No schedule", "2"))) {
            mvc.perform(get("/api/roles").param("sort", "nextScheduled").param("size", "1").param("page", expected.getValue()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].title").value(expected.getKey()));
        }
        mvc.perform(patch(noSchedule).header("Host", "localhost:8080").header("Origin", "http://evil.example")
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0,\"title\":\"Wrong\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("origin_rejected"));
        mvc.perform(patch(noSchedule).header("Host", "evil.example").contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"title\":\"Wrong\"}")).andExpect(status().isForbidden());
        mvc.perform(patch(noSchedule).header("Host", "localhost:8080").contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("json_required"));
        mvc.perform(localJson(post(later + "/assignments"), """
            {"title":"Invalid date","dueOn":"2026-02-30"}
            """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.dueOn").exists());
        mvc.perform(localJson(post(later + "/assignments"), """
            {"title":"Missing date"}
            """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.dueOn").value("Enter a due date."));
        mvc.perform(localJson(post(later + "/interviews"), """
            {"title":"Invalid instant","type":"FINAL","status":"SCHEDULED","startsAt":1795165200}
            """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.startsAt").exists());
        mvc.perform(get("/api/missing")).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void concurrentUpdatesSerializeOnTheRoleVersion() throws Exception {
        String role = createRole("Orbit", "Analyst");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> first = concurrentPatch(role, "APPLIED", ready, start);
            Callable<Integer> second = concurrentPatch(role, "IN_PROGRESS", ready, start);
            var firstResult = executor.submit(first);
            var secondResult = executor.submit(second);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        mvc.perform(get(role + "/timeline")).andExpect(jsonPath("$.total").value(2));
    }

    private Callable<Integer> concurrentPatch(String role, String roleStatus, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) throw new TimeoutException();
            return mvc.perform(localJson(patch(role), "{\"expectedVersion\":0,\"status\":\"" + roleStatus + "\"}"))
                    .andReturn().getResponse().getStatus();
        };
    }

    private String createRole(String company, String title) throws Exception {
        return mvc.perform(localJson(post("/api/roles"), json.writeValueAsString(Map.of("company", company, "title", title))))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
    }

    private JsonNode perform(MockHttpServletRequestBuilder request, String body, int statusCode) throws Exception {
        var result = mvc.perform(localJson(request, body)).andExpect(status().is(statusCode)).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray());
    }

    private static MockHttpServletRequestBuilder localJson(MockHttpServletRequestBuilder request, String body) {
        return request.header("Host", "localhost:8080").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static long version(JsonNode node) {
        return node.path("version").asLong();
    }

    private static String versionBody(JsonNode node) {
        return "{\"expectedVersion\":" + version(node) + "}";
    }
}

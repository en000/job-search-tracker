package local.jobtracker;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
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
        sql.update("update workspaces set time_zone = 'UTC'");
    }

    @Test
    void completionSuggestionsSnoozeDismissAndResetOnProgress() throws Exception {
        String role = createRole("Orbit", "Analyst");
        createRole("Orbit", "Saved role");
        var applied = perform(patch(role), """
            {"expectedVersion":0,"stage":"APPLIED"}
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
        assertThat(snoozed.path("stage").asText()).isEqualTo("APPLIED");
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
            {"expectedVersion":%d,"stage":"IN_PROGRESS","substage":"INTERVIEW_SCHEDULED"}
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
            {"expectedVersion":0,"stage":"IN_PROGRESS","substage":"FIRST_INTERVIEW_PASSED"}
            """, 200);
        long stageVersion = version(progressed);
        var exactNoOp = perform(patch(role), """
            {"expectedVersion":%d,"stage":"IN_PROGRESS"}
            """.formatted(stageVersion), 200);
        assertThat(exactNoOp).isEqualTo(progressed);
        perform(patch(role), """
            {"expectedVersion":%d,"stage":"APPLIED","substage":"FIRST_INTERVIEW_PASSED"}
            """.formatted(stageVersion), 400);
        var moved = perform(patch(role), """
            {"expectedVersion":%d,"stage":"APPLIED"}
            """.formatted(stageVersion), 200);
        assertThat(moved.path("substage").isNull()).isTrue();
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
            {"expectedVersion":0,"stage":"IN_PROGRESS","substage":"ASSIGNMENT_PENDING"}
            """, 200);
        var assignment = perform(post(role + "/assignments"), """
            {"title":"Exercise","dueOn":"2026-11-16","brief":"Keep original instructions","workUrl":"https://example.com/repo"}
            """, 201);
        var interview = perform(post(role + "/interviews"), """
            {"title":"Final interview","type":"FINAL","status":"SCHEDULED","startsAt":"2026-11-17T12:00:00Z"}
            """, 201);
        var submitted = perform(put(role + "/assignments/" + assignment.path("id").asText()), """
            {"expectedVersion":0,"title":"Exercise","status":"SUBMITTED","dueOn":"2026-11-16",
             "brief":"Keep original instructions","workUrl":"https://example.com/repo","feedback":"Clear solution"}
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
        assertThat(reopened.path("substage").asText()).isEqualTo("ASSIGNMENT_PENDING");
        mvc.perform(get("/api/today")).andExpect(jsonPath("$.upcoming[0].kind").value("SCHEDULED_INTERVIEW"))
                .andExpect(jsonPath("$.dueToday.length()").value(0));
        mvc.perform(get("/api/export")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=job-search-tracker.json"))
                .andExpect(jsonPath("$.schemaVersion").value(1)).andExpect(jsonPath("$.roles[0].title").value("Analyst"))
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

    private Callable<Integer> concurrentPatch(String role, String stage, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) throw new TimeoutException();
            return mvc.perform(localJson(patch(role), "{\"expectedVersion\":0,\"stage\":\"" + stage + "\"}"))
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

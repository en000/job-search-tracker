package com.jobtracker.domain;

import jakarta.persistence.*;
import com.jobtracker.domain.enums.Theme;

@Entity
@Table(name = "app_settings")
public class AppSettings {
    @Id
    public Integer id;
    public String timeZone;
    @Enumerated(EnumType.STRING)
    public Theme theme;
}

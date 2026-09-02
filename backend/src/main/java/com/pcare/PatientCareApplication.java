package com.pcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 360 Patient Care, Medical Assistance, Transport &amp; Care Coordination Platform.
 *
 * <p>One central backend exposing governed REST APIs consumed by the Admin Command Centre
 * portal (React) and the role-based Android apps (Patient, Care Agent). Everything is
 * organised around a single Patient Case ID so that every operational, healthcare,
 * communication and financial event is traceable to one case.</p>
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class PatientCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientCareApplication.class, args);
    }
}

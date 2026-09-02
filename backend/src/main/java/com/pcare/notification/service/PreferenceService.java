package com.pcare.notification.service;

import com.pcare.notification.domain.NotificationPreference;
import com.pcare.notification.repo.NotificationPreferenceRepository;
import com.pcare.notification.web.dto.NotificationDtos.PreferenceDto;
import com.pcare.notification.web.dto.NotificationDtos.UpsertPreferenceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/** Per-user notification channel/category preferences. */
@Service
public class PreferenceService {

    private final NotificationPreferenceRepository repository;

    public PreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PreferenceDto> forUser(Long userId) {
        return repository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public PreferenceDto upsert(UpsertPreferenceRequest req) {
        NotificationPreference p = repository.findByUserIdAndCategory(req.userId(), req.category())
                .orElseGet(NotificationPreference::new);
        p.setUserId(req.userId());
        p.setCategory(req.category());
        p.setEnabled(req.enabled());
        p.setChannels(req.channels() != null ? new HashSet<>(req.channels()) : new HashSet<>());
        return toDto(repository.save(p));
    }

    private PreferenceDto toDto(NotificationPreference p) {
        return new PreferenceDto(p.getId(), p.getUserId(), p.getCategory(), p.isEnabled(), p.getChannels());
    }
}

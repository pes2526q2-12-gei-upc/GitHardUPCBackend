package com.safesteps.backend.domain.users.dto;

import com.safesteps.backend.domain.users.model.Premi;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PremiDTO {
    private String id;
    private String url;

    public PremiDTO(Premi p) {
        this.id = p.getId();
        this.url = p.getUrl();
    }
}

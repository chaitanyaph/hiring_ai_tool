package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.NoteType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrengthWeaknessResponse {
    private NoteType noteType;
    private String description;
}

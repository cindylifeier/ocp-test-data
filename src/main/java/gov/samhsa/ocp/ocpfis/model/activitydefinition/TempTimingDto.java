package gov.samhsa.ocp.ocpfis.model.activitydefinition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempTimingDto {
    private String durationMax;
}

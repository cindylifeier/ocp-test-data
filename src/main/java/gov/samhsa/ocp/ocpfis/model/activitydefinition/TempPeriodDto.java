package gov.samhsa.ocp.ocpfis.model.activitydefinition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempPeriodDto {

    private String start;

    private String end;
}

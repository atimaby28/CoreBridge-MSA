package halo.corebridge.jobpostinghot.model.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HotJobpostingId implements Serializable {
    private LocalDate dateKey;
    private Long jobpostingId;
}

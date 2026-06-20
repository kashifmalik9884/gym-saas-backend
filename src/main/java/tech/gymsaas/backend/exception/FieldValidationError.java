package tech.gymsaas.backend.exception;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldValidationError {
    private String field;
    private String message;
}
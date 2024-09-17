
package ru.practicum.statistic.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "apps")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class App {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_id")
    @JsonIgnore
    private Long id;

    @Column(name = "app_name")
    @NotBlank
    private String name;
}

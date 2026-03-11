package com.inkFront.schoolManagement.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_subjects", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"className", "subject_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String className;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
}
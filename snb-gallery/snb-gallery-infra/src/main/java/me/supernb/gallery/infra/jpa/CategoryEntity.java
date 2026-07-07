package me.supernb.gallery.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// gallery.category 行实体:三轴类目(scene/style/subject)。收录管线 SQL 写入,JPA 侧只读。
@Entity
@Table(name = "category", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "slug")
    private String slug;

    @Column(name = "axis")
    private String axis;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "name_zh")
    private String nameZh;

    @Column(name = "sort")
    private Integer sort;
}

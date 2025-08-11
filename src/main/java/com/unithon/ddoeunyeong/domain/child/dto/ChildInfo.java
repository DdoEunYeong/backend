package com.unithon.ddoeunyeong.domain.child.dto;

import java.time.LocalDate;

public record ChildInfo(
        Long id,
        String name,
        String gender,
        Long age,
        String characteristic,
        int adviceCount,
        LocalDate lastAdviceDate,
        LocalDate birthday,
        String childImage,
        String dollImage
        ) {
}

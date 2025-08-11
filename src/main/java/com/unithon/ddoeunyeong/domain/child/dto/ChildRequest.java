package com.unithon.ddoeunyeong.domain.child.dto;

import java.time.LocalDate;

public record ChildRequest(String name, LocalDate birthDate, String character, String gender) {
}

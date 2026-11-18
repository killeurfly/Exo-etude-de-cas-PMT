package com.exo_PMT.pmt_backend.dto;

import java.time.LocalDate;

public record CreateProjectRequest(String name, String description, LocalDate startDate) {}

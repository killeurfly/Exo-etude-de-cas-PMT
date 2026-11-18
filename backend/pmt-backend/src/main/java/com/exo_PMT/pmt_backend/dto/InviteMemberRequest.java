package com.exo_PMT.pmt_backend.dto;

import com.exo_PMT.pmt_backend.model.Role;

public record InviteMemberRequest(String email, Role role) {}

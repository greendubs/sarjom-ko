package com.sarjom.citisci.dtos;

import com.sarjom.citisci.bos.ProjectBO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InviteUserResponseDTO {
    public String status;
    public ProjectBO projectBO;
}

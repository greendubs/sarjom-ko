package com.sarjom.citisci.services.impl;

import com.sarjom.citisci.bos.ProjectBO;
import com.sarjom.citisci.bos.UserBO;
import com.sarjom.citisci.db.mongo.daos.IProjectDAO;
import com.sarjom.citisci.db.mongo.daos.IUserProjectMappingDAO;
import com.sarjom.citisci.dtos.CreateProjectRequestDTO;
import com.sarjom.citisci.dtos.CreateProjectResponseDTO;
import com.sarjom.citisci.dtos.FetchAllProjectsForUserResponseDTO;
import com.sarjom.citisci.entities.Project;
import com.sarjom.citisci.entities.UserProjectMapping;
import com.sarjom.citisci.enums.ProjectType;
import com.sarjom.citisci.enums.Role;
import com.sarjom.citisci.services.IProjectService;
import com.sarjom.citisci.services.transactional.IProjectTransService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements IProjectService {
    private static Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Autowired
    IProjectDAO projectDAO;
    @Autowired
    IUserProjectMappingDAO userProjectMappingDAO;
    @Autowired
    IProjectTransService projectTransService;

    @Override
    public CreateProjectResponseDTO createProject(CreateProjectRequestDTO createProjectRequestDTO, UserBO userBO) throws Exception {
        logger.info("Inside createProject");

        if (userBO == null || userBO.getRole() == null || !userBO.getRole().equals(Role.COLLECTOR)) {
            throw new Exception("User not authorized to create projects");
        }

        validateCreateProjectRequestDTO(createProjectRequestDTO);

        checkThatProjectWithThisNameDoesntExist(createProjectRequestDTO);

        ProjectBO projectBO = createProjectBO(createProjectRequestDTO);

        CreateProjectResponseDTO createProjectResponseDTO = projectTransService.createProject(projectBO, true);

        return createProjectResponseDTO;
    }

    private void checkThatProjectWithThisNameDoesntExist(CreateProjectRequestDTO createProjectRequestDTO) throws Exception {
        logger.info("Inside checkThatProjectWithThisNameDoesntExist");

        List<Project> projectsWithGivenName = projectDAO.getProjectsByName(createProjectRequestDTO.getName());

        if (!CollectionUtils.isEmpty(projectsWithGivenName)) {
            throw new Exception("There is already a project with this name");
        }
    }

    private ProjectBO createProjectBO(CreateProjectRequestDTO createProjectRequestDTO) throws Exception {
        logger.info("Inside createProjectBO");

        ProjectBO projectBO = new ProjectBO();

        BeanUtils.copyProperties(createProjectRequestDTO, projectBO);
        projectBO.setId(new ObjectId().toHexString());

        return projectBO;
    }

    private void validateCreateProjectRequestDTO(CreateProjectRequestDTO createProjectRequestDTO) throws Exception {
        logger.info("Inside validateCreateProjectRequestDTO");

        if (createProjectRequestDTO == null ||
                StringUtils.isEmpty(createProjectRequestDTO.getOrganisationId()) ||
                StringUtils.isEmpty(createProjectRequestDTO.getName()) ||
                StringUtils.isEmpty(createProjectRequestDTO.getCreatedByUserId()) ||
                StringUtils.isEmpty(createProjectRequestDTO.getLicense()) ||
                createProjectRequestDTO.getProjectType() == null) {
            throw new Exception("Invalid create project request");
        }
    }

    @Override
    public FetchAllProjectsForUserResponseDTO fetchAllProjectsForUser(UserBO userBO) throws Exception {
        logger.info("Inside fetchAllProjectsForUser");

        if (userBO == null || StringUtils.isEmpty(userBO.getId())) {
            throw new Exception("Please login again");
        }

        FetchAllProjectsForUserResponseDTO fetchAllProjectsForUserResponseDTO = new FetchAllProjectsForUserResponseDTO();
        List<ProjectBO> projectBOs = new ArrayList<>();

        fetchAllProjectsForUserResponseDTO.setProjects(projectBOs);

        List<UserProjectMapping> userProjectMappings = userProjectMappingDAO.fetchByUserId(new ObjectId(userBO.getId()));

        if (CollectionUtils.isEmpty(userProjectMappings)) {
            return fetchAllProjectsForUserResponseDTO;
        }

        List<ObjectId> projectIds = userProjectMappings.stream().map(UserProjectMapping::getProjectId).collect(Collectors.toList());

        List<Project> projects = projectDAO.fetchByIds(projectIds);

        if (CollectionUtils.isEmpty(projects)) {
            return fetchAllProjectsForUserResponseDTO;
        }

        populateProjectBOs(projects, projectBOs);

        return fetchAllProjectsForUserResponseDTO;
    }

    private void populateProjectBOs(List<Project> projects, List<ProjectBO> projectBOs) {
        logger.info("Inside populateProjectBOs");

        for (Project project: projects) {
            if (project == null || StringUtils.isEmpty(project.getName()) ||
                project.getId() == null || project.getOrganisationId() == null ||
                project.getCreatedByUserId() == null || StringUtils.isEmpty(project.getProjectType())) {
                continue;
            }

            projectBOs.add(convertToProjectBO(project));
        }
    }

    private ProjectBO convertToProjectBO(Project project) {
        logger.info("Inside convertToProjectBO");

        ProjectBO projectBO = new ProjectBO();

        BeanUtils.copyProperties(project, projectBO);

        projectBO.setId(project.getId().toHexString());
        projectBO.setOrganisationId(project.getOrganisationId().toHexString());
        projectBO.setCreatedByUserId(project.getCreatedByUserId().toHexString());
        projectBO.setProjectType(ProjectType.valueOf(project.getProjectType()));

        return projectBO;
    }
}

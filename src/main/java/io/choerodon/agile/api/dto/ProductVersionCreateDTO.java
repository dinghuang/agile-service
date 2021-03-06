package io.choerodon.agile.api.dto;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Created by jian_zhang02@163.com on 2018/5/14.
 */

public class ProductVersionCreateDTO {
    private static final String NAME_NULL_ERROR = "error.productVersionName.NotNull";
    private static final String PROJECT_ID_NULL_ERROR = "error.projectId.NotNull";

    @NotNull(message = NAME_NULL_ERROR)
    private String name;
    private String description;
    private Date startDate;
    private Date releaseDate;
    @NotNull(message = PROJECT_ID_NULL_ERROR)
    private Long projectId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

}

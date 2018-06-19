package io.choerodon.agile.api.controller.v1;

import io.choerodon.agile.api.dto.DataLogDTO;
import io.choerodon.agile.app.service.DataLogService;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Created by HuangFuqiang@choerodon.io on 2018/6/14.
 * Email: fuqianghuang01@gmail.com
 */
@RestController
@RequestMapping(value = "/v1/project/{project_id}/data_log")
public class DataLogController {

    @Autowired
    private DataLogService dataLogService;

    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("创建DataLog")
    @PostMapping
    public ResponseEntity<DataLogDTO> createDataLog(@ApiParam(value = "项目id", required = true)
                                                     @PathVariable(name = "project_id") Long projectId,
                                                     @ApiParam(value = "data log object", required = true)
                                                     @RequestBody DataLogDTO dataLogDTO) {
        return Optional.ofNullable(dataLogService.create(projectId, dataLogDTO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.dataLog.create"));
    }

    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("查询DataLog列表")
    @GetMapping
    public ResponseEntity<List<DataLogDTO>> listByIssueId(@ApiParam(value = "项目id", required = true)
                                                          @PathVariable(name = "project_id") Long projectId,
                                                          @ApiParam(value = "issue id", required = true)
                                                          @RequestParam Long issueId) {
        return Optional.ofNullable(dataLogService.listByIssueId(projectId, issueId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.dataLogList.get"));
    }

}

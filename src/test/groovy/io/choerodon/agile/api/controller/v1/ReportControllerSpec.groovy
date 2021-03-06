package io.choerodon.agile.api.controller.v1

import com.alibaba.fastjson.JSONObject
import io.choerodon.agile.AgileTestConfiguration
import io.choerodon.agile.api.dto.BurnDownReportCoordinateDTO
import io.choerodon.agile.api.dto.BurnDownReportDTO
import io.choerodon.agile.api.dto.CumulativeFlowDiagramDTO
import io.choerodon.agile.api.dto.CumulativeFlowFilterDTO
import io.choerodon.agile.api.dto.IssueBurnDownReportDTO
import io.choerodon.agile.api.dto.IssueCreateDTO
import io.choerodon.agile.api.dto.IssueDTO
import io.choerodon.agile.api.dto.IssueListDTO
import io.choerodon.agile.api.dto.IssueUpdateDTO
import io.choerodon.agile.api.dto.PieChartDTO
import io.choerodon.agile.api.dto.ReportIssueDTO
import io.choerodon.agile.api.dto.SprintBurnDownReportDTO
import io.choerodon.agile.api.dto.SprintDetailDTO
import io.choerodon.agile.api.dto.SprintUpdateDTO
import io.choerodon.agile.api.dto.VelocitySprintDTO
import io.choerodon.agile.api.dto.VersionIssueRelDTO
import io.choerodon.agile.app.service.IssueService
import io.choerodon.agile.app.service.ReportService
import io.choerodon.agile.app.service.SprintService
import io.choerodon.agile.domain.agile.repository.UserRepository
import io.choerodon.agile.infra.common.utils.MybatisFunctionTestUtil
import io.choerodon.agile.infra.dataobject.GroupDataChartDO
import io.choerodon.agile.infra.dataobject.GroupDataChartListDO
import io.choerodon.agile.infra.dataobject.IssueChangeDO
import io.choerodon.agile.infra.dataobject.SprintDO
import io.choerodon.agile.infra.dataobject.UserDO
import io.choerodon.agile.infra.dataobject.UserMessageDO
import io.choerodon.agile.infra.dataobject.VersionIssueChangeDO
import io.choerodon.agile.infra.mapper.BoardColumnMapper
import io.choerodon.agile.infra.mapper.IssueMapper
import io.choerodon.agile.infra.mapper.ReportMapper
import io.choerodon.agile.infra.mapper.SprintMapper
import io.choerodon.agile.infra.mapper.VersionIssueRelMapper
import io.choerodon.core.domain.Page
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import static org.mockito.Matchers.anyString
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

/**
 *
 * @author dinghuang123@gmail.com
 * @since 2018/8/27
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AgileTestConfiguration)
@ActiveProfiles("test")
@Stepwise
class ReportControllerSpec extends Specification {

    @Autowired
    @Qualifier("mockUserRepository")
    private UserRepository userRepository

    @Autowired
    private TestRestTemplate restTemplate

    @Autowired
    private SprintService sprintService

    @Autowired
    private IssueService issueService

    @Autowired
    private BoardColumnMapper boardColumnMapper

    @Autowired
    private SprintMapper sprintMapper

    @Autowired
    private IssueMapper issueMapper

    @Autowired
    private VersionIssueRelMapper versionIssueRelMapper

    @Autowired
    private ReportService reportService

    @Autowired
    private ReportMapper reportMapper

    @Shared
    def projectId = 1

    @Shared
    def epicId = 1

    @Shared
    def boardId = 1

    @Shared
    def versionId = 1

    @Shared
    def sprintId = null

    @Shared
    def issueIds = []

    @Shared
    def startDate = MybatisFunctionTestUtil.dataSubFunction(new Date(), -1)

    @Shared
    def endDate = MybatisFunctionTestUtil.dataSubFunction(startDate, -10)

    @Mock
    private ValueOperations valueOperations

    @Mock
    RedisTemplate redisTemplate

    def setup() {
        given: '设置feign调用mockito'
        // *_表示任何长度的参数（这里表示只要执行了queryUsersMap这个方法，就让它返回一个空的Map
        Map<Long, UserMessageDO> userMessageDOMap = new HashMap<>()
        UserMessageDO userMessageDO = new UserMessageDO("管理员", "http://XXX.png", "dinghuang123@gmail.com")
        userMessageDOMap.put(1, userMessageDO)
        userRepository.queryUsersMap(*_) >> userMessageDOMap
        UserDO userDO = new UserDO()
        userDO.setRealName("管理员")
        userRepository.queryUserNameByOption(*_) >> userDO

        MockitoAnnotations.initMocks(this)
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations)
        Mockito.doNothing().when(valueOperations).set(anyString(), anyString())
    }

    def 'createSprintToStart'() {
        given: '创建一个冲刺'
        SprintDetailDTO sprintDetailDTO = sprintService.createSprint(1)
        sprintId = sprintDetailDTO.sprintId

        and: '将issue加入到冲刺中'
        IssueCreateDTO issueCreateDTO = new IssueCreateDTO()
        issueCreateDTO.projectId = projectId
        issueCreateDTO.sprintId = sprintId
        issueCreateDTO.summary = '加入冲刺issue'
        issueCreateDTO.typeCode = 'story'
        issueCreateDTO.priorityCode = 'low'
        issueCreateDTO.reporterId = 1
        IssueDTO issueDTO = issueService.createIssue(issueCreateDTO)
        issueIds.add(issueDTO.issueId)

        and: '将issue设置为done状态'
        IssueUpdateDTO issueUpdateDTO = new IssueUpdateDTO()
        issueUpdateDTO.statusId = 3
        issueUpdateDTO.issueId = issueIds[0]
        issueUpdateDTO.objectVersionNumber = issueDTO.objectVersionNumber
        issueService.updateIssue(projectId, issueUpdateDTO, ["statusId"])

        and: '创建不为done的issue'
        IssueCreateDTO noDone = new IssueCreateDTO()
        noDone.projectId = projectId
        noDone.sprintId = sprintId
        noDone.summary = '加入冲刺issue'
        noDone.typeCode = 'story'
        noDone.priorityCode = 'low'
        noDone.reporterId = 1
        IssueDTO noDoneIssue = issueService.createIssue(issueCreateDTO)
        issueIds.add(noDoneIssue.issueId)

        and: '设置冲刺开启对象'
        SprintUpdateDTO sprintUpdateDTO = new SprintUpdateDTO()
        sprintUpdateDTO.sprintId = sprintId
        sprintUpdateDTO.projectId = projectId
        sprintUpdateDTO.objectVersionNumber = sprintDetailDTO.objectVersionNumber
        sprintUpdateDTO.startDate = startDate
        sprintUpdateDTO.endDate = endDate

        when: '将冲刺开启'
        SprintDetailDTO startSprint = sprintService.startSprint(projectId, sprintUpdateDTO)

        then: '验证冲刺是否开启成功'
        startSprint.statusCode == 'started'
    }

    def 'queryBurnDownReport'() {
        when: '向开始查询冲刺对应的燃尽图报告信息的接口发请求'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/{sprintId}/burn_down_report?type={type}',
                List, projectId, sprintId, type)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<ReportIssueDTO> reportIssueDTOList = entity.body

        expect: '验证期望值'
        reportIssueDTOList.size() == expectSize

        where: '设置期望值'
        type                     | expectSize
        'storyPoints'            | 2
        'remainingEstimatedTime' | 2
        'issueCount'             | 2
        'xxx'                    | 2

    }

    def 'queryBurnDownCoordinate'() {
        when: '向开始查询燃尽图坐标信息的接口发请求'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/{sprintId}/burn_down_report/coordinate?type={type}',
                JSONObject, projectId, sprintId, type)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        JSONObject object = entity.body
        TreeMap<String, Integer> report = object.get("coordinate") as TreeMap<String, Integer>

        expect: '验证期望值'
        report.size() == expectSize
        object.get("expectCount") == exceptCount

        where: '设置期望值'
        type                     || expectSize | exceptCount
        'storyPoints'            || 1          | 0
        'remainingEstimatedTime' || 1          | 0
        'issueCount'             || 1          | 1

    }

    def 'queryCumulativeFlowDiagram'() {
        given: '查询参数'
        CumulativeFlowFilterDTO cumulativeFlowFilterDTO = new CumulativeFlowFilterDTO()
        cumulativeFlowFilterDTO.startDate = startDate
        cumulativeFlowFilterDTO.endDate = endDate
        cumulativeFlowFilterDTO.boardId = 1

        and: '加入列'
        cumulativeFlowFilterDTO.columnIds = boardColumnMapper.queryColumnIdsByBoardId(boardId, projectId)

        when: '向开始查看项目累积流量图的接口发请求'
        def entity = restTemplate.postForEntity('/v1/projects/{project_id}/reports/cumulative_flow_diagram',
                cumulativeFlowFilterDTO, List, projectId)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<CumulativeFlowDiagramDTO> result = entity.body

        expect: '验证期望值'
        result.size() == 3

    }

    def 'queryIssueByOptions'() {
        when: '向根据状态查版本下issue列表的接口发请求'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/{versionId}/issues?' +
                'status={status}&type={type}', Page, projectId, versionId, status, type)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<IssueListDTO> result = entity.body.content

        expect: '验证期望值'
        result.size() == expectSize

        where: '设置期望值'
        status                  | type                     || expectSize
        'done'                  | 'storyPoints'            || 0
        'done'                  | 'remainingEstimatedTime' || 0
        'done'                  | 'issueCount'             || 0
        'unfinished'            | 'storyPoints'            || 1
        'unfinished'            | 'remainingEstimatedTime' || 0
        'unfinished'            | 'issueCount'             || 1
        'unfinishedUnestimated' | 'storyPoints'            || 0
        'unfinishedUnestimated' | 'remainingEstimatedTime' || 1
        'unfinishedUnestimated' | 'issueCount'             || 0

    }

    def 'queryBurnDownCoordinateByType'() {
        given: 'issue加入到版本和epic中'
        IssueUpdateDTO issueUpdateDTO = new IssueUpdateDTO()
        issueUpdateDTO.issueId = issueIds[0]
        issueUpdateDTO.setObjectVersionNumber(issueMapper.selectByPrimaryKey(issueIds[0]).getObjectVersionNumber())
        if (type == 'Epic') {
            issueUpdateDTO.epicId = id
            issueUpdateDTO.storyPoints = 1
            issueService.updateIssue(projectId, issueUpdateDTO, ["epicId", "storyPoints"])
        } else {
            VersionIssueRelDTO versionIssueRelDTO = new VersionIssueRelDTO()
            versionIssueRelDTO.issueId = issueIds[0]
            versionIssueRelDTO.versionId = id
            versionIssueRelDTO.projectId = projectId
            List<VersionIssueRelDTO> versionIssueRelDTOList = new ArrayList<>()
            versionIssueRelDTOList.add(versionIssueRelDTO)
            issueUpdateDTO.versionIssueRelDTOList = versionIssueRelDTOList
            issueUpdateDTO.versionType = "fix"
            issueService.updateIssue(projectId, issueUpdateDTO, [])
        }

        when: 'Epic和版本燃耗图坐标信息'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/burn_down_coordinate_type/{id}?type={type}', List, projectId, id, type)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<BurnDownReportCoordinateDTO> burnDownReportCoordinateDTOList = entity.body

        expect: '验证期望值'
        burnDownReportCoordinateDTOList.size() == expectSize

        where: '设置期望值'
        type      | id || expectSize
        'Epic'    | 1  || 2
        'Version' | 1  || 2
    }

    def 'queryVersionLineChart'() {
        given: 'mock issueMapper'
        def reportMapperMock = Mock(ReportMapper)
        reportService.setReportMapper(reportMapperMock)
        List<VersionIssueChangeDO> versionIssueChangeDOList = new ArrayList<>()
        VersionIssueChangeDO versionIssueChangeDO = new VersionIssueChangeDO()
        versionIssueChangeDO.addIssueIds = [1]
        versionIssueChangeDO.removeIssueIds = [2]
        versionIssueChangeDOList.add(versionIssueChangeDO)
        List<IssueChangeDO> issueChangeDOS = new ArrayList<>()
        IssueChangeDO issueChangeDO = new IssueChangeDO()
        issueChangeDO.changeDate = new Date()
        issueChangeDO.issueId = 1
        issueChangeDO.issueNum = 'AG-1'
        issueChangeDO.oldValue = '1'
        issueChangeDO.newValue = '2'
        issueChangeDO.status = 'todo'
        issueChangeDO.changeField = '1'
        issueChangeDO.typeCode = 'story'
        issueChangeDO.completed = false
        issueChangeDOS.add(issueChangeDO)

        when: '向版本报告图信息的接口发请求'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/{versionId}?type={type}', Map, projectId, versionId, type)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        Map<String, Object> result = entity.body
        Object productVersionDO = result.get("version")
        List<Object> versionReportDTOList = result.get("versionReport") as List<Object>
        reportService.setReportMapper(reportMapper)

        and: '判断mock交互并且设置返回值'
        if (type == 'issueCount') {
            1 * reportMapperMock.queryCompletedIssueCount(_, _) >> 1
        } else {
            1 * reportMapperMock.queryTotalField(projectId, _, _) >> 3
            1 * reportMapperMock.queryCompleteField(projectId, _, _) >> 1
            1 * reportMapperMock.queryUnEstimateCount(projectId, _, _) >> 2
            1 * reportMapperMock.queryChangeFieldIssue(projectId, _, _) >> issueChangeDOS
        }
        4 * reportMapperMock.queryChangIssue(projectId, _, _) >> issueChangeDOS
        1 * reportMapperMock.queryIssueIdByVersionId(projectId, _) >> [1]
        1 * reportMapperMock.queryChangeIssue(projectId, _, _, _) >> versionIssueChangeDOList
        2 * reportMapperMock.queryCompletedChangeIssue(projectId, _, _) >> versionIssueChangeDOList

        expect: '验证期望值'
        productVersionDO != null
        versionReportDTOList.size() == expectSize

        where: '设置期望值'
        type                     || expectSize
        'storyPoints'            || 2
        'remainingEstimatedTime' || 2
        'issueCount'             || 2

    }

    def 'queryVelocityChart'() {
        when: '向速度图的接口发请求'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/velocity_chart?type={type}', List, projectId, type)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<VelocitySprintDTO> velocitySprintDTOList = entity.body

        expect: '验证期望值'
        velocitySprintDTOList.size() == expectSize

        where: '设置期望值'
        type          || expectSize
        'issue_count' || 2
        'story_point' || 2
        'remain_time' || 2

    }

    def 'queryPieChart'() {
        when: '向查询饼图的接口发请求'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/pie_chart?fieldName={fieldName}', List, projectId, fieldName)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<PieChartDTO> pieChartDTOList = entity.body

        expect: '验证期望值'
        pieChartDTOList.size() == expectSize

        where: '设置期望值'
        fieldName      || expectSize
        'assignee'     || 1
        'component'    || 1
        'typeCode'     || 2
        'version'      || 2
        'priorityCode' || 2
        'statusCode'   || 2
        'sprint'       || 3
        'epic'         || 1
        'resolution'   || 2

    }

    def 'queryEpicChart'() {
        when: '向史诗图的接口发请求'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/epic_chart?epicId={epicId}&type={type}', List, projectId, epicId, type)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<GroupDataChartDO> groupDataChartDOList = entity.body

        expect: '验证期望值'
        groupDataChartDOList.size() == expectSize

        where: '设置期望值'
        type          || expectSize
        'issue_count' || 1
        'story_point' || 1
        'remain_time' || 1
    }

    def 'epic_issue_list'() {
        when: '向史诗图问题列表的接口发请求'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/epic_issue_list?epicId={epicId}', List, projectId, epicId)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<GroupDataChartListDO> groupDataChartListDOList = entity.body

        expect: '验证期望值'
        groupDataChartListDOList.size() == 2
    }

    def 'version_chart'() {
        when: '向版本图重构api的接口发请求'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/version_chart?versionId={versionId}&type={type}', List, projectId, versionId, type)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<GroupDataChartDO> groupDataChartDOList = entity.body

        expect: '验证期望值'
        groupDataChartDOList.size() == expectSize

        where: '设置期望值'
        type          || expectSize
        'issue_count' || 1
        'story_point' || 1
        'remain_time' || 1
    }

    def 'version_issue_list'() {
        when: '版本图问题列表重构api'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/version_issue_list?versionId={versionId}', List, projectId, versionId)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<GroupDataChartListDO> groupDataChartListDOList = entity.body

        expect: '验证期望值'
        groupDataChartListDOList.size() == 2

    }

    def 'queryBurnDownReportByType'() {
        when: 'Epic和版本燃耗图报告信息'
        def entity = restTemplate.getForEntity('/v1/projects/{project_id}/reports/burn_down_report_type/{id}?type={type}', BurnDownReportDTO, projectId, id, type)

        then: '接口是否请求成功'
        entity.statusCode.is2xxSuccessful()

        and: '设置返回值值'
        List<SprintBurnDownReportDTO> sprintBurnDownReportDTOList = entity.body.sprintBurnDownReportDTOS
        List<IssueBurnDownReportDTO> incompleteIssues = entity.body.incompleteIssues

        expect: '验证期望值'
        sprintBurnDownReportDTOList.size() == expectSizeOne
        incompleteIssues.size() == expectSizeTwo

        where: '设置期望值'
        type      | id || expectSizeOne | expectSizeTwo
        'Epic'    | 1  || 1             | 0
        'Version' | 1  || 1             | 0

    }

    def 'deleteData'() {
        given: '删除数据DO'
        SprintDO sprintDO = new SprintDO()
        sprintDO.sprintId = sprintId
        sprintDO.projectId = projectId

        when: '删除数据'
        sprintMapper.delete(sprintDO)
        issueService.deleteIssue(projectId, issueIds[0])
        issueService.deleteIssue(projectId, issueIds[1])

        then: '验证'
        sprintMapper.selectByPrimaryKey(sprintDO) == null
        issueMapper.selectByPrimaryKey(issueIds[0]) == null
        issueMapper.selectByPrimaryKey(issueIds[1]) == null


    }

}

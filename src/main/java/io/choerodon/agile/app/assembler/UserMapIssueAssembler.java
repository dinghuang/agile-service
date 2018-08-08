package io.choerodon.agile.app.assembler;

import io.choerodon.agile.api.dto.UserMapIssueDTO;
import io.choerodon.agile.domain.agile.repository.UserRepository;
import io.choerodon.agile.infra.common.utils.ColorUtil;
import io.choerodon.agile.infra.dataobject.LookupValueDO;
import io.choerodon.agile.infra.dataobject.UserMapIssueDO;
import io.choerodon.agile.infra.dataobject.UserMessageDO;
import io.choerodon.agile.infra.mapper.LookupValueMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by HuangFuqiang@choerodon.io on 2018/8/8.
 * Email: fuqianghuang01@gmail.com
 */
@Component
public class UserMapIssueAssembler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LookupValueMapper lookupValueMapper;

    private static final String ISSUE_STATUS_COLOR = "issue_status_color";

    public List<UserMapIssueDTO> userMapIssueDOToDTO(List<UserMapIssueDO> userMapIssueDOList) {
        LookupValueDO lookupValueDO = new LookupValueDO();
        lookupValueDO.setTypeCode(ISSUE_STATUS_COLOR);
        List<UserMapIssueDTO> userMapIssueDTOList = new ArrayList<>();
        Map<String, String> lookupValueMap = lookupValueMapper.select(lookupValueDO).stream().collect(Collectors.toMap(LookupValueDO::getValueCode, LookupValueDO::getName));
        List<Long> assigneeIds = userMapIssueDOList.stream().filter(issue -> issue.getAssigneeId() != null && !Objects.equals(issue.getAssigneeId(), 0L)).map(UserMapIssueDO::getAssigneeId).distinct().collect(Collectors.toList());
        Map<Long, UserMessageDO> usersMap = userRepository.queryUsersMap(assigneeIds, true);
        userMapIssueDOList.forEach(userMapIssueDO -> {
            String assigneeName = usersMap.get(userMapIssueDO.getAssigneeId()) != null ? usersMap.get(userMapIssueDO.getAssigneeId()).getName() : null;
            String imageUrl = assigneeName != null ? usersMap.get(userMapIssueDO.getAssigneeId()).getImageUrl() : null;
            UserMapIssueDTO userMapIssueDTO = new UserMapIssueDTO();
            BeanUtils.copyProperties(userMapIssueDO, userMapIssueDTO);
            userMapIssueDTO.setStatusColor(ColorUtil.initializationStatusColor(userMapIssueDTO.getStatusCode(), lookupValueMap));
            userMapIssueDTO.setAssigneeName(assigneeName);
            userMapIssueDTO.setImageUrl(imageUrl);
            userMapIssueDTOList.add(userMapIssueDTO);
        });
        return userMapIssueDTOList;
    }
}

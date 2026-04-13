package com.yzh.yingshi.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class BehaviorEventDetailVO extends BehaviorEventVO {
    private Long taskId;
    private JsonNode ruleDetail;
}

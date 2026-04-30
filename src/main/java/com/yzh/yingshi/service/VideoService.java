package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.CloudPlaybackUrlRequestDTO;
import com.yzh.yingshi.dto.CloudRecordQueryDTO;
import com.yzh.yingshi.dto.LiveUrlRequestDTO;
import com.yzh.yingshi.vo.CloudPlaybackUrlVO;
import com.yzh.yingshi.vo.CloudRecordFileVO;
import com.yzh.yingshi.vo.LiveUrlVO;

import java.util.List;

public interface VideoService {

    LiveUrlVO getLiveUrl(LiveUrlRequestDTO dto);

    List<CloudRecordFileVO> listCloudRecords(CloudRecordQueryDTO dto);

    CloudPlaybackUrlVO getCloudPlaybackUrl(CloudPlaybackUrlRequestDTO dto);
}

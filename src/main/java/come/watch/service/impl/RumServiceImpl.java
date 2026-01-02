package come.watch.service.impl;

import come.watch.mapper.RumPoMapper;
import come.watch.repository.RumPo;
import come.watch.service.RumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RumServiceImpl implements RumService {
    private final RumPoMapper rumPoMapper;
    @Override
    public void collect(RumPo dto, String userAgent) {
        RumPo entity = new RumPo();
        entity.setClientTs(dto.getClientTs());
        entity.setMetric(dto.getMetric());
        entity.setValue(dto.getValue());
        entity.setDelta(dto.getDelta());
        entity.setMetricId(dto.getMetricId());
        entity.setRating(dto.getRating());
        entity.setNavType(dto.getNavType());
        entity.setRouteKey(dto.getRouteKey());
        entity.setReleaseVer(dto.getReleaseVer());
        entity.setEnv(dto.getEnv());
        entity.setDevice(dto.getDevice());
        entity.setNetwork(dto.getNetwork());
        entity.setUa(userAgent);
        entity.setDeleted(0);

        rumPoMapper.insert(entity);
    }
}

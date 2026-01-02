package come.watch.service;

import come.watch.repository.RumPo;

import javax.servlet.http.HttpServletRequest;

public interface RumService {
    void collect(RumPo body, String userAgent, HttpServletRequest req);

    void collectBatch(RumPo[] dtos, String userAgent, HttpServletRequest req);
}

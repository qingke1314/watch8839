package come.watch.service;

import come.watch.repository.RumPo;

public interface RumService {
    public void collect(RumPo body, String userAgent);
}

package come.watch.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourceSummaryExt {
    public List<TopnResourceItem> td; // top by duration
    public List<TopnResourceItem> ts; // top by size
}

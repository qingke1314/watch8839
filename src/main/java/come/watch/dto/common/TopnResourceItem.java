package come.watch.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopnResourceItem {
    private int d; // duration
    private int s; // transferSize
    public String k;    // 归一化 key（你上报时最好就归一化）
    public String t;  // script/css/img/font/other
    public String host;       // 可空
    public int metric;        // dur=ms 或 size=bytes
    private int st; // startTime
    private String r; // renderBlockingStatus
}

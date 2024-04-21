package cn.youyou.yyregistry.cluster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"url"})
public class Server {
    private String url;
    private boolean status;
    private boolean leader;
    private long version;
}

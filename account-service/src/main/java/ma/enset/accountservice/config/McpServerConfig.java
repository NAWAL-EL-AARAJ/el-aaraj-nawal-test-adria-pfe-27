package ma.enset.accountservice.config;

import ma.enset.accountservice.mcp.AccountMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider accountTools(AccountMcpTools accountMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(accountMcpTools)
                .build();
    }
}

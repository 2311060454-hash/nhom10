package vn.shop.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import vn.shop.common.ApiException;
import static org.assertj.core.api.Assertions.*;

class GatewayControllerTest {
    @Test void rejectsUnknownRoute() {
        var request=new MockHttpServletRequest("GET","/api/unknown");
        assertThatThrownBy(() -> new GatewayController().proxy(request)).isInstanceOf(ApiException.class).hasMessage("Không tìm thấy dữ liệu");
    }
    @Test void rejectsTraversalBeforeNetworkCall() {
        var request=new MockHttpServletRequest("GET","/api/auth/../internal/sessions");
        assertThatThrownBy(() -> new GatewayController().proxy(request)).isInstanceOf(ApiException.class).hasMessage("Đường dẫn không hợp lệ");
    }
}

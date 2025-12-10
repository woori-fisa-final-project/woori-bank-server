package dev.woori.wooriBank.domain.transfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferRequestDto;
import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferResponseDto;
import dev.woori.wooriBank.domain.transaction.transfer.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
public class TransferControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    TransferService transferService; // ★ Service는 Mock 처리

    @Test
    @DisplayName("정상 이체 요청 → 200 OK + JSON 응답 검증")
    void transfer_success() throws Exception {

        // given
        TransferRequestDto request = new TransferRequestDto(
                "999900000001",
                "111100000001",
                50000L
        );

        TransferResponseDto response = TransferResponseDto.builder()
                .fromAccount("999900000001")
                .toAccount("111100000001")
                .amount(50000L)
                .message("이체가 성공적으로 처리되었습니다.")
                .build();

        given(transferService.transfer(any())).willReturn(response);

        // when & then
        mockMvc.perform(
                        post("/api/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAccount").value("999900000001"))
                .andExpect(jsonPath("$.toAccount").value("111100000001"))
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.message").value("이체가 성공적으로 처리되었습니다."));
    }

    @Test
    @DisplayName("유효성 검증 실패(금액 0원) → 400 Bad Request")
    void validation_fail_amount() throws Exception {

        TransferRequestDto request = new TransferRequestDto(
                "999900000001",
                "111100000001",
                0L
        );

        mockMvc.perform(
                        post("/api/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Service에서 CommonException 발생 → 400 Bad Request")
    void service_exception_common() throws Exception {

        TransferRequestDto request = new TransferRequestDto(
                "999900000001",
                "111100000001",
                50000L
        );

        // service.transfer() 호출 시 예외 발생하도록 설정
        doThrow(new CommonException(ErrorCode.INVALID_REQUEST, "보내는 계좌와 받는 계좌가 동일할 수 없습니다."))
                .when(transferService).transfer(any());

        mockMvc.perform(
                        post("/api/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Service 내부 예상치 못한 에러 → 500 Internal Server Error")
    void service_runtime_exception() throws Exception {

        TransferRequestDto request = new TransferRequestDto(
                "999900000001",
                "111100000001",
                50000L
        );

        // Mocking: Unexpected exception
        doThrow(new RuntimeException("서버 내부 오류"))
                .when(transferService).transfer(any());

        mockMvc.perform(
                        post("/api/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isInternalServerError());
    }
}
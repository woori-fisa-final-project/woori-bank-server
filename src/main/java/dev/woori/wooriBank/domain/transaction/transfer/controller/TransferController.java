package dev.woori.wooriBank.domain.transaction.transfer.controller;

import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferRequestDto;
import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferResponseDto;
import dev.woori.wooriBank.domain.transaction.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 입금 기능을 외부에서 호출할 수 있게 해주는 REST API 컨트롤러.
 *
 * "/api/transfer" 경로로 POST 요청이 들어오면 입금 처리 실행.
 *
 * Controller → Service 로 요청을 전달하고
 * Service에서 반환한 TransferResponseDto를 HTTP Response로 반환한다.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;


    /**
     *      입금 실행
     *      POST /api/transfer
     *      - 요청 Body: TransferRequestDto(JSON)
     *      - 응답 Body: TransferResponseDto(JSON)
     */

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponseDto> transfer(@Valid @RequestBody TransferRequestDto request) {
        //디버깅 및 추적용
        log.info("[계좌이체 요청]: {}", request);

        TransferResponseDto response = transferService.transfer(request); // DepositService에 입금 처리 위임
        return ResponseEntity.ok(response); // 200 OK + 입금 결과 반환
    }
}

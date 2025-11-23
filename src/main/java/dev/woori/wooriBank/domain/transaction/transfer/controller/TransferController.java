package dev.woori.wooriBank.domain.transaction.transfer.controller;

import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferRequestDto;
import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferResponseDto;
import dev.woori.wooriBank.domain.transaction.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 입금 기능을 외부에서 호출할 수 있게 해주는 REST API 컨트롤러.
 *
 * "/api/deposit" 경로로 POST 요청이 들어오면 입금 처리 실행.
 *
 * Controller → Service 로 요청을 전달하고
 * Service에서 반환한 DepositResponseDto를 HTTP Response로 반환한다.
 */

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * POST /api/deposit
     *
     * 요청 바디(JSON)를 DepositRequestDto로 받아 입금 처리.
     */

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponseDto> transfer(@RequestBody TransferRequestDto request) {
        TransferResponseDto response = transferService.transfer(request); // DepositService에 입금 처리 위임
        return ResponseEntity.ok(response); // 200 OK + 입금 결과 반환
    }
}

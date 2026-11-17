package com.alexandrelp7.finance.web;

import com.alexandrelp7.finance.bank.ArkeaPsd2Connector;
import com.alexandrelp7.finance.bank.BankConnectionRepository;
import com.alexandrelp7.finance.bank.BankSyncService;
import com.alexandrelp7.finance.bank.CsvImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bank")
public class BankController {

  private final BankSyncService syncService;
  private final CsvImportService csvImportService;
  private final BankConnectionRepository connectionRepository;

  public BankController(
      BankSyncService syncService,
      CsvImportService csvImportService,
      BankConnectionRepository connectionRepository) {
    this.syncService = syncService;
    this.csvImportService = csvImportService;
    this.connectionRepository = connectionRepository;
  }

  @GetMapping("/connect")
  public ConnectResponse connect() {
    return new ConnectResponse(syncService.connector().buildConsentUrl(null));
  }

  @PostMapping("/callback")
  public ResponseEntity<Void> callback(@Valid @RequestBody CallbackRequest request) {
    syncService.connector().handleCallback(request.code());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/sync")
  public BankSyncService.SyncResult sync() {
    return syncService.sync();
  }

  @PostMapping("/accounts/{accountId}/import/csv")
  public CsvImportService.ImportResult importCsv(
      @PathVariable Long accountId, @RequestParam("file") MultipartFile file) {
    return csvImportService.importCsv(accountId, file);
  }

  @GetMapping("/status")
  public StatusResponse status() {
    boolean connected = connectionRepository.existsByProvider(ArkeaPsd2Connector.PROVIDER);
    return new StatusResponse(connected, ArkeaPsd2Connector.PROVIDER);
  }

  public record ConnectResponse(String url) {}

  public record CallbackRequest(@NotBlank String code, String state) {}

  public record StatusResponse(boolean connected, String provider) {}
}

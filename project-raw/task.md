# Task: Phase 2 - Remote Monitor & Real Unlock

## Context
Phase 1 (Basic RFCOMM) is done.
Phase 2 combines:
1.  **System Monitor**: Broadcasting CPU/RAM usage to Android.
2.  **Real Unlock**: Android sends the Windows PIN, and Windows simulates keystrokes to unlock.

## Roadmap
- [ ] **Windows: Metrics Collection**
    - [ ] Create `MetricCollector` (PDH/Memory).
    - [ ] Broadcast `METRICS:CPU=xx;RAM=yy`.
- [ ] **Android: Metrics UI**
    - [ ] Parse `METRICS` packet.
    - [ ] Display circular progress bars.
- [ ] **Real Unlock Implementation**
    - [ ] **Android**: Add "Set Windows PIN" input (Secure Storage).
    - [ ] **Android**: Update "Unlock" button to send `UNLOCK:<PIN>`.
    - [ ] **Windows**: Implement `InputSimulator` (SendInput API).
    - [ ] **Windows**: On `UNLOCK:<PIN>` -> Simulate Keypresses + Enter.

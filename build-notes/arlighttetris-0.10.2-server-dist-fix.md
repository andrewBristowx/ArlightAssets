# ArlightTetris 0.10.2 — Server Dist Fix

El crash de servidor dedicado se producía porque `NetworkSetup` enlazaba directamente `ClientPacketHandler`, que a su vez carga `Minecraft` y `Screen`.

La versión 0.10.2 incorpora `ClientPacketBridge`, un puente común sin clases cliente. La implementación real se instala únicamente desde `ClientSetup`, marcado para `Dist.CLIENT`.

Validación:
- ArlightCore 1.25.4 compilado.
- ArlightTetris 0.10.2 compilado.
- `NetworkSetup` contiene referencias a `ClientPacketBridge` y ninguna referencia a `ClientPacketHandler`.
- GitHub Actions: ejecución 31062826028, resultado correcto.
- SHA-256 del JAR: `496f31608ec58cc1718547af9e4a5a18a88eb3322ae505595eff1e372945c760`.

# Pedilo

Repositorio activo de la app Pedilo.

La unica fuente documental vigente es:

- `ESTADO_ACTUAL_PEDILO.md`

No usar documentacion historica, reportes viejos, evidencias, auditorias, dictamenes, capturas ni archivos eliminados como referencia activa. Si una decision no esta en el codigo actual o en `ESTADO_ACTUAL_PEDILO.md`, no debe asumirse vigente.

## Validacion local

```bash
./gradlew compileDebugKotlin
bash tools/guards/check_ui_quality.sh
node --test tests/*.test.js
```

## Alcance del repo activo

El repo debe contener solo codigo funcional actual, configuracion necesaria, backend/functions/reglas vigentes, assets necesarios, tests/guards utiles, este README y `ESTADO_ACTUAL_PEDILO.md`.

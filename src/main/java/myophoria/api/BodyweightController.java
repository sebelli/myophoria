package myophoria.api;

import io.javalin.config.RoutesConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.Unit;
import myophoria.core.service.BodyweightService;

/** HTTP adapter for bodyweight entries. */
public class BodyweightController {
    /** Request body for create and update. */
    public record BodyweightRequest(double weight, Unit unit, Instant recordedAt, LocalDate localDate) {
    }

    /** Entry as sent to the browser. */
    public record BodyweightDto(String id, double weight, Unit unit, Instant recordedAt, LocalDate localDate) {
        static BodyweightDto of(BodyweightEntry e) {
            return new BodyweightDto(e.getId(), e.getWeight(), e.getUnit(), e.getRecordedAt(), e.getLocalDate());
        }
    }

    private final BodyweightService service;

    /** Creates the controller on the service. */
    public BodyweightController(BodyweightService service) {
        this.service = service;
    }

    /** Registers the routes. */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/bodyweight", ctx -> {
            List<BodyweightDto> result = new ArrayList<>();
            for (BodyweightEntry e : service.list()) {
                result.add(BodyweightDto.of(e));
            }
            ctx.json(result);
        });
        routes.get("/api/v1/bodyweight/latest", ctx -> {
            BodyweightEntry latest = service.latest();
            if (latest == null) {
                ctx.status(204);
            } else {
                ctx.json(BodyweightDto.of(latest));
            }
        });
        routes.post("/api/v1/bodyweight", ctx -> {
            BodyweightRequest r = ctx.bodyAsClass(BodyweightRequest.class);
            ctx.status(201).json(BodyweightDto.of(service.create(r.weight(), r.unit(), r.recordedAt(), r.localDate())));
        });
        routes.put("/api/v1/bodyweight/{id}", ctx -> {
            BodyweightRequest r = ctx.bodyAsClass(BodyweightRequest.class);
            ctx.json(BodyweightDto.of(
                    service.update(ctx.pathParam("id"), r.weight(), r.unit(), r.recordedAt(), r.localDate())));
        });
        routes.delete("/api/v1/bodyweight/{id}", ctx -> {
            service.delete(ctx.pathParam("id"));
            ctx.status(204);
        });
    }
}

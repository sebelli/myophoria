package myophoria.api;

import io.javalin.config.RoutesConfig;
import java.util.ArrayList;
import java.util.List;
import myophoria.core.model.Gym;
import myophoria.core.service.GymService;

/** HTTP adapter for gyms. */
public class GymController {
    /** Request body for create and rename. */
    public record GymRequest(String name) {
    }

    /** Gym as sent to the browser. */
    public record GymDto(String id, String name, boolean isDefault) {
        static GymDto of(Gym g) {
            return new GymDto(g.getId(), g.getName(), g.isDefault());
        }
    }

    private final GymService service;

    /** Creates the controller on the service. */
    public GymController(GymService service) {
        this.service = service;
    }

    /** Registers the routes. */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/gyms", ctx -> {
            List<GymDto> result = new ArrayList<>();
            for (Gym g : service.list()) {
                result.add(GymDto.of(g));
            }
            ctx.json(result);
        });
        routes.get("/api/v1/gyms/active", ctx -> ctx.json(GymDto.of(service.activeGym())));
        routes.post("/api/v1/gyms", ctx -> {
            GymRequest r = ctx.bodyAsClass(GymRequest.class);
            ctx.status(201).json(GymDto.of(service.create(r.name())));
        });
        routes.put("/api/v1/gyms/{id}", ctx -> {
            GymRequest r = ctx.bodyAsClass(GymRequest.class);
            ctx.json(GymDto.of(service.rename(ctx.pathParam("id"), r.name())));
        });
        routes.post("/api/v1/gyms/{id}/default", ctx -> ctx.json(GymDto.of(service.makeDefault(ctx.pathParam("id")))));
        routes.delete("/api/v1/gyms/{id}", ctx -> {
            service.delete(ctx.pathParam("id"));
            ctx.status(204);
        });
    }
}

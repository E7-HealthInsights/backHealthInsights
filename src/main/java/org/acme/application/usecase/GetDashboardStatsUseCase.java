package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.acme.application.dto.DashboardStatsResponseDto;

@ApplicationScoped
public class GetDashboardStatsUseCase {

    @Inject EntityManager em;

    public DashboardStatsResponseDto execute() {
        DashboardStatsResponseDto dto = new DashboardStatsResponseDto();

        // Usuarios via fn_total_usuarios
        dto.setUsuariosActivos(      callFnUsuarios(null, true));
        dto.setUsuariosInactivos(    callFnUsuarios(null, false));
        dto.setUsuariosPorRolAdmin(  callFnUsuarios(1, true));
        dto.setUsuariosPorRolDG(     callFnUsuarios(2, true));
        dto.setUsuariosPorRolDF(     callFnUsuarios(3, true));
        dto.setUsuariosPorRolDM(     callFnUsuarios(4, true));

        // Datasets via fn_total_datasets — cada estado 
        dto.setDatasetsActivos(      callFnDatasets("READY"));
        dto.setDatasetsInactivos(  callFnDatasets("INACTIVE"));

        return dto;
    }

    private int callFnUsuarios(Integer rolId, boolean status) {
        String sql = rolId == null
            ? "SELECT fn_total_usuarios(NULL, :status)"
            : "SELECT fn_total_usuarios(:rolId, :status)";

        var query = em.createNativeQuery(sql)
                .setParameter("status", status);
        if (rolId != null) query.setParameter("rolId", rolId);

        return ((Number) query.getSingleResult()).intValue();
    }

    private int callFnDatasets(String estado) {
        return ((Number) em.createNativeQuery(
            "SELECT fn_total_datasets(:estado)")
            .setParameter("estado", estado)
            .getSingleResult()
        ).intValue();
    }
}
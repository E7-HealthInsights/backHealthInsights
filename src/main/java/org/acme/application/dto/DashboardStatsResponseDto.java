package org.acme.application.dto;

public class DashboardStatsResponseDto {
    private int usuariosActivos;
    private int usuariosInactivos;
    private int usuariosPorRolAdmin;
    private int usuariosPorRolDG;
    private int usuariosPorRolDF;
    private int usuariosPorRolDM;
    private int datasetsActivos;
    private int datasetsInactivos;

    // getters y setters
    public int getUsuariosActivos()       { return usuariosActivos; }
    public void setUsuariosActivos(int v) { this.usuariosActivos = v; }

    public int getUsuariosInactivos()       { return usuariosInactivos; }
    public void setUsuariosInactivos(int v) { this.usuariosInactivos = v; }

    public int getUsuariosPorRolAdmin()       { return usuariosPorRolAdmin; }
    public void setUsuariosPorRolAdmin(int v) { this.usuariosPorRolAdmin = v; }

    public int getUsuariosPorRolDG()       { return usuariosPorRolDG; }
    public void setUsuariosPorRolDG(int v) { this.usuariosPorRolDG = v; }

    public int getUsuariosPorRolDF()       { return usuariosPorRolDF; }
    public void setUsuariosPorRolDF(int v) { this.usuariosPorRolDF = v; }

    public int getUsuariosPorRolDM()       { return usuariosPorRolDM; }
    public void setUsuariosPorRolDM(int v) { this.usuariosPorRolDM = v; }

    public int getDatasetsActivos()       { return datasetsActivos; }
    public void setDatasetsActivos(int v) { this.datasetsActivos = v; }

    public int getDatasetsInactivos()       { return datasetsInactivos; }
    public void setDatasetsInactivos(int v) { this.datasetsInactivos = v; }
}
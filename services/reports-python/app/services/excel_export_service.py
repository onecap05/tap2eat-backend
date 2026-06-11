from datetime import date
from io import BytesIO

from openpyxl import Workbook

from app.schemas.dashboard_schema import OwnerDashboardResponse


class ExcelExportService:
    def build_owner_dashboard_workbook(
        self,
        dashboard: OwnerDashboardResponse,
        from_date: date,
        to_date: date
    ) -> BytesIO:
        workbook = Workbook()

        summary_sheet = workbook.active
        summary_sheet.title = "Resumen"

        summary_sheet.append(["Reporte Tap2Eat"])
        summary_sheet.append(["Restaurante", dashboard.restaurant_id])
        summary_sheet.append(["Fecha inicio", from_date.isoformat()])
        summary_sheet.append(["Fecha fin", to_date.isoformat()])
        summary_sheet.append([])

        summary_sheet.append(["Pedidos", "Valor"])
        summary_sheet.append(["Total de pedidos", dashboard.orders.total_orders])
        summary_sheet.append(["Pedidos cobrables", dashboard.orders.sales_order_count])
        summary_sheet.append(["Ventas totales", dashboard.orders.total_sales])
        summary_sheet.append(["Ventas entregadas", dashboard.orders.delivered_sales])
        summary_sheet.append(["Ticket promedio", dashboard.orders.average_ticket])
        summary_sheet.append(["Pedidos creados", dashboard.orders.created_orders])
        summary_sheet.append(["Pedidos aceptados", dashboard.orders.accepted_orders])
        summary_sheet.append(["Pedidos en preparación", dashboard.orders.preparing_orders])
        summary_sheet.append(["Pedidos listos", dashboard.orders.ready_orders])
        summary_sheet.append(["Pedidos entregados", dashboard.orders.delivered_orders])
        summary_sheet.append(["Pedidos cancelados", dashboard.orders.cancelled_orders])

        summary_sheet.append([])
        summary_sheet.append(["Catálogo", "Valor"])
        summary_sheet.append(["Total de productos", dashboard.catalog.total_products])
        summary_sheet.append(["Productos disponibles", dashboard.catalog.available_products])
        summary_sheet.append(["Productos pausados", dashboard.catalog.paused_products])
        summary_sheet.append(["Productos simples", dashboard.catalog.simple_products])
        summary_sheet.append(["Productos personalizables", dashboard.catalog.customizable_products])
        summary_sheet.append(["Productos destacados", dashboard.catalog.featured_products])
        summary_sheet.append([
            "Productos con horario personalizado",
            dashboard.catalog.products_with_custom_schedule
        ])
        summary_sheet.append(["Total de categorías", dashboard.catalog.total_categories])
        summary_sheet.append(["Categorías activas", dashboard.catalog.active_categories])

        status_sheet = workbook.create_sheet("Pedidos por estado")
        status_sheet.append(["Estado", "Total"])
        for metric in dashboard.orders.orders_by_status:
            status_sheet.append([metric.status, metric.total])

        product_type_sheet = workbook.create_sheet("Productos por tipo")
        product_type_sheet.append(["Tipo", "Total"])
        for metric in dashboard.catalog.products_by_type:
            product_type_sheet.append([metric.product_type, metric.total])

        product_status_sheet = workbook.create_sheet("Productos por estado")
        product_status_sheet.append(["Estado", "Total"])
        for metric in dashboard.catalog.products_by_status:
            product_status_sheet.append([metric.status, metric.total])

        output = BytesIO()
        workbook.save(output)
        output.seek(0)

        return output
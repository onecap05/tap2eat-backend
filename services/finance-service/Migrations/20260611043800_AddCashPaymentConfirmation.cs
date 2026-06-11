using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace FinanceService.Migrations;

public partial class AddCashPaymentConfirmation : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.AddColumn<decimal>(
            name: "AmountReceived",
            table: "payments",
            type: "numeric(18,2)",
            precision: 18,
            scale: 2,
            nullable: true);

        migrationBuilder.AddColumn<decimal>(
            name: "ChangeAmount",
            table: "payments",
            type: "numeric(18,2)",
            precision: 18,
            scale: 2,
            nullable: true);
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropColumn(
            name: "AmountReceived",
            table: "payments");

        migrationBuilder.DropColumn(
            name: "ChangeAmount",
            table: "payments");
    }
}

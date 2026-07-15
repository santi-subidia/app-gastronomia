using ApiGastronomia.Domain.Enums;

namespace ApiGastronomia.Tests.Data;

public class EstadoPedidoEnumTests
{
    [Fact]
    public void Devuelto_HasValueEight()
    {
        // Assert: the enum value maps to integer 8
        Assert.Equal(8, (int)EstadoPedidoEnum.Devuelto);
    }
}
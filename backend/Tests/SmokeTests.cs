namespace ApiGastronomia.Tests;

public class SmokeTests
{
    [Fact]
    public void Domain_Assembly_Loads()
    {
        var type = typeof(ApiGastronomia.Domain.Entities.Usuario);
        Assert.NotNull(type);
        Assert.Equal("Usuario", type.Name);
    }
}

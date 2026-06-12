using System.ComponentModel.DataAnnotations;
using System.Reflection;
using ApiGastronomia.Controllers;

namespace ApiGastronomia.Tests.Controllers;

public class RequestValidationTests
{
    /// <summary>
    /// In .NET 10+ ASP.NET Core, validation attributes on record types must be on
    /// constructor parameters, not properties. These tests verify attributes are
    /// properly placed on the parameters that ASP.NET Core model binding uses.
    /// </summary>

    [Fact]
    public void LoginRequest_Password_HasMinLengthValidationOnParameter()
    {
        var passwordParam = typeof(LoginRequest)
            .GetConstructors()[0]
            .GetParameters()[1]; // Password is second parameter

        var attr = passwordParam
            .GetCustomAttributes(typeof(MinLengthAttribute), false)
            .Cast<MinLengthAttribute>()
            .FirstOrDefault();

        Assert.NotNull(attr);
        Assert.Equal(6, attr.Length);
    }

    [Fact]
    public void LoginRequest_Password_RejectsShortValues()
    {
        var attr = new MinLengthAttribute(6);
        Assert.False(attr.IsValid(""));
        Assert.False(attr.IsValid("ab"));
        Assert.False(attr.IsValid("12345"));
        Assert.True(attr.IsValid("123456"));
        Assert.True(attr.IsValid("validPassword123"));
    }

    [Fact]
    public void CreateUserRequest_Password_HasMinLengthValidationOnParameter()
    {
        var passwordParam = typeof(CreateUserRequest)
            .GetConstructors()[0]
            .GetParameters()[1]; // Password is second parameter

        var attr = passwordParam
            .GetCustomAttributes(typeof(MinLengthAttribute), false)
            .Cast<MinLengthAttribute>()
            .FirstOrDefault();

        Assert.NotNull(attr);
        Assert.Equal(6, attr.Length);
    }

    [Fact]
    public void CreateUserRequest_Password_RejectsShortValues()
    {
        var attr = new MinLengthAttribute(6);
        Assert.False(attr.IsValid(""));
        Assert.False(attr.IsValid("12345"));
        Assert.True(attr.IsValid("123456"));
    }
}

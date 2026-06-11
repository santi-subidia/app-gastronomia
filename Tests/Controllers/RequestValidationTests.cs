using System.ComponentModel.DataAnnotations;
using ApiGastronomia.Controllers;

namespace ApiGastronomia.Tests.Controllers;

public class RequestValidationTests
{
    [Theory]
    [InlineData("")]
    [InlineData("ab")]
    [InlineData("12345")]
    public void LoginRequest_WithShortPassword_FailsValidation(string shortPassword)
    {
        var request = new LoginRequest("testuser", shortPassword);
        var results = new List<ValidationResult>();
        var context = new ValidationContext(request);

        var isValid = Validator.TryValidateObject(request, context, results, validateAllProperties: true);

        Assert.False(isValid);
        Assert.Contains(results, r => r.MemberNames.Contains(nameof(LoginRequest.Password)));
    }

    [Fact]
    public void LoginRequest_WithValidPassword_PassesValidation()
    {
        var request = new LoginRequest("testuser", "validPassword123");
        var results = new List<ValidationResult>();
        var context = new ValidationContext(request);

        var isValid = Validator.TryValidateObject(request, context, results, validateAllProperties: true);

        Assert.True(isValid);
    }

    [Theory]
    [InlineData("")]
    [InlineData("12345")]
    public void CreateUserRequest_WithShortPassword_FailsValidation(string shortPassword)
    {
        var request = new CreateUserRequest("newuser", shortPassword, 1);
        var results = new List<ValidationResult>();
        var context = new ValidationContext(request);

        var isValid = Validator.TryValidateObject(request, context, results, validateAllProperties: true);

        Assert.False(isValid);
        Assert.Contains(results, r => r.MemberNames.Contains(nameof(CreateUserRequest.Password)));
    }
}

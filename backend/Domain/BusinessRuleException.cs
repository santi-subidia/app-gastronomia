namespace ApiGastronomia.Domain;

public sealed class BusinessRuleException : InvalidOperationException
{
    public BusinessRuleException(string code, string message)
        : base(message)
    {
        Code = code;
    }

    public string Code { get; }
}

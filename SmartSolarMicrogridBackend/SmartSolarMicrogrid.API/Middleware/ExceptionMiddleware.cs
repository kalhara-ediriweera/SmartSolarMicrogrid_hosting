using System.Text.Json;
namespace SmartSolarMicrogrid.API.Middleware;
public class ExceptionMiddleware {
    private readonly RequestDelegate _next;
    public ExceptionMiddleware(RequestDelegate next)=>_next=next;
    public async Task Invoke(HttpContext context){
        try{await _next(context);}
        catch(Exception ex){
            var status=ex switch{UnauthorizedAccessException=>401,KeyNotFoundException=>404,ArgumentException=>400,InvalidOperationException=>409,_=>500};
            context.Response.StatusCode=status;context.Response.ContentType="application/json";
            await context.Response.WriteAsync(JsonSerializer.Serialize(new{error=ex.Message,status}));
        }
    }
}

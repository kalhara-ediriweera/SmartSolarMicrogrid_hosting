using SmartSolarMicrogrid.Application.Validators;
using Xunit;
namespace SmartSolarMicrogrid.Tests;
public class ValidationTests {
    [Fact] public void PositiveRejectsZero()=>Assert.Throws<ArgumentException>(()=>Validation.Positive(0,"x"));
    [Fact] public void CoordinatesRejectInvalid()=>Assert.Throws<ArgumentException>(()=>Validation.LatitudeLongitude(100,0));
    [Fact] public void RequiredRejectsBlank()=>Assert.Throws<ArgumentException>(()=>Validation.Required(" ","x"));
}
